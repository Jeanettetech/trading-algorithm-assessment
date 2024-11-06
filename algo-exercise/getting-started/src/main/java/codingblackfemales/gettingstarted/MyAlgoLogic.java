package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* Welcome to my High Frequency Trading Algorithm.
*
* This algorithm assesses the market liquidity or volatility via the spread to determine whether conditions are prime to
* buy and sell or cancel.
*
* If a spread percentage under the set target limit is recorded and a minimum of two filled orders have been executed,
* the algo will utilise a High Frequency Trading indicator named Volume imbalance and a VWAP benchmark to assess market
* conditions to create child orders.
*
* If conditions are met child buy orders will be created at the current best bid price with the matching volume.
*
* In regard to the child sell orders; they will be created at the best ask price with a participation rate 25% of the
* average volume of executed buy orders. The Inline sell volume will fluctuate depending on the spread percentage.
*
* Lastly when the target spread percentage is not met, active child orders will be cancelled depending on whether
* they meet the best bid or ask price.
*/

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    @Override
    public Action evaluate(SimpleAlgoState state) {

        var orderBookAsString = Util.orderBookToString(state);

        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);


        var totalOrderCount = state.getChildOrders().size();

        //make sure we have an exit condition...
        if (totalOrderCount > 20) {
            return NoAction.NoAction;
        }


        final AskLevel ask = state.getAskAt(0);
        long bestAskQuantity = ask.quantity;
        long bestAskPrice = ask.price;

        final BidLevel bid = state.getBidAt(0);
        long bestBidQuantity = bid.quantity;
        long bestBidPrice = bid.price;


        logger.info("[MYALGO] Best Bid: Quantity = " + bid.quantity + ", Price = " + bid.price);
        logger.info("[MYALGO] Best Ask: Quantity = " + ask.quantity + ", Price = " + ask.price);


        logger.info("[MYALGO] Bid-ask spread percentage is: " + String.format("%.2f", bidAskSpreadPercentage(state)) + "%");

        final var filledStateOrders = state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).toList().size();
        final int minimumFilledOrderQuantity = 2;
        final double targetSpreadLimit = 4.5; //preferably would be set to under 1% when executing high frequency trading strategies-
        /*based on research a spread of less than 1% is preferred in real life markets when executing high frequency trading strategies.
        However, to accommodate the order book data provided I have set a target spread limit of 4.5%*/


        if (bidAskSpreadPercentage(state) < targetSpreadLimit) {
            logger.info("[MYALGO] Bid-Ask Spread percentage is less than " + targetSpreadLimit + "%. Market is liquid. Evaluating child orders...");
            childOrderLog(state);
            profitTracker(state);


            if (filledStateOrders < minimumFilledOrderQuantity) {//Before creating competitive buy or sell child orders a VWAP benchmark calculated from a minimum of two filled orders needs to be attained
                logger.info("[MYALGO] Have: " + filledStateOrders + " filled orders. Adding aggressive Buy order: " + bestAskQuantity + " @ " + bestAskPrice);
                return new CreateChildOrder(Side.BUY, bestAskQuantity, bestAskPrice);
            } else {
                logger.info("[MYALGO] Achieved " + minimumFilledOrderQuantity + " filled order minimum. Evaluating conditions for competitive orders...");
            }

            double VWAP = calculateVWAP(state);

            final var buyChildOrders = state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.BUY)).toList();
            final var sellChildOrders = state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.SELL)).toList();


            logger.info("[MYALGO] VWAP calculated as: " + String.format("%.2f", VWAP)+ ", Volume Imbalance indicating: " + volumeImbalanceIndicator(state));
            boolean buyOrder = false;
            boolean sellOrder = false;


            if (filledBuyStateSize(state) < 5 && (bestBidPrice <= VWAP &&  volumeImbalanceIndicator(state).contains("NEGATIVE"))) {
                logger.info("[MYALGO] Have " + buyChildOrders.size() + " Buy orders. VWAP and Volume Imbalance conditions have been met. Adding Buy order for " + bestBidQuantity + " @ " + bestBidPrice);
                buyOrder = true; //create buy child order if the volume imbalance indicates as negative as it indicates an imminent drop in midpoint due to increased selling pressure.

            }
            if (calculateSharesRemainingToSell(state) > 0 && (bestAskPrice >= VWAP &&  volumeImbalanceIndicator(state).contains("POSITIVE"))) {
                logger.info("[MYALGO] Have " + sellChildOrders.size() + " Sell orders. VWAP and Volume Imbalance conditions have been met. Adding Sell order for " + sellVolumeInline(state,25.0) + " @ " + bestAskPrice);
                sellOrder = true; //create sell child order if the volume imbalance indicates as positive as it indicates an imminent increase in midpoint due to increased buying pressure.
                //adopting a participation rate of 25% of the average volume of executed buy orders as the sell quantity as part of exit the strategy to ensure a discrete exit with aims of impacting the market as little as possible.
            }
            if (buyOrder) {
                return new CreateChildOrder(Side.BUY, bestBidQuantity, bestBidPrice);
            }
            if (sellOrder) {
                return new CreateChildOrder(Side.SELL, sellVolumeInline(state,25.0), bestAskPrice);
            }
            logger.info("[MYALGO] Conditions for Buy or Sell orders have not been met. No action taken.");
            return NoAction.NoAction;

        } else {
            logger.info("[MYALGO] Bid-Ask Spread percentage is greater than " + targetSpreadLimit + "%. Market is volatile. Evaluating active child orders for cancellation...");

            childOrderLog(state);
            profitTracker(state);
            for (ChildOrder orders : state.getActiveChildOrders()) {//will cancel the first non-viable active child older that does not meet the best bid or ask due to no longer being competitive
                if (orders.getSide().equals(Side.BUY) && orders.getState() != OrderState.FILLED && orders.getPrice() != bestBidPrice) {
                    logger.info("[MYALGO] Cancelling non-viable Buy order: ID = " + orders.getOrderId() + ", Quantity = " + orders.getQuantity() + ", Price = " + orders.getPrice());
                    return new CancelChildOrder(orders);

                }
                if (orders.getSide().equals(Side.SELL) && orders.getState() != OrderState.FILLED && orders.getPrice() != bestAskPrice) {
                    logger.info("[MYALGO] Cancelling non-viable Sell order : ID = " + orders.getOrderId() + ", Quantity = " + orders.getQuantity() + ", Price = " + orders.getPrice());
                    logger.info(state.getActiveChildOrders().toString());
                    return new CancelChildOrder(orders);
                }
            }
        }
        logger.info("[MYALGO] No non-viable orders, no action taken.");
        return NoAction.NoAction;

    }

    protected double bidAskSpreadPercentage(SimpleAlgoState state) {
        final AskLevel ask = state.getAskAt(0);
        long bestAskPrice = ask.price;

        final BidLevel bid = state.getBidAt(0);
        long bestBidPrice = bid.price;
        return (double) (bestAskPrice - bestBidPrice) / (bestAskPrice) * 100;

    }

    protected double calculateVWAP(SimpleAlgoState state) {

        double totalQuantity = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).mapToDouble(ChildOrder::getFilledQuantity).sum();
        double totalPriceQuantity =  state.getChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).mapToDouble(order -> order.getFilledQuantity() * order.getPrice()).sum();


        return totalQuantity == 0 ? 0 : totalPriceQuantity / totalQuantity;

    }

    protected double calculateVolumeImbalance(SimpleAlgoState state) {
        final AskLevel ask = state.getAskAt(0);
        final BidLevel bid = state.getBidAt(0);

        return (double) (bid.quantity - ask.quantity) / (bid.quantity + ask.quantity);
    }


    private String analyzedVolumeImbalance(double volumeImbalance) {

        String formattedVolumeImbalance = String.format("%.2f", volumeImbalance);
      //the calculated volume imbalance value will range from -1 to 1
        if (volumeImbalance >= -1 && volumeImbalance <= -0.3) {
            return "NEGATIVE(" + formattedVolumeImbalance + "), POSSIBLE SELL PRESSURE";

        } else if (volumeImbalance > -0.3 && volumeImbalance < 0.3) {
            return "NEUTRAL(" + formattedVolumeImbalance + ")";

        } else if (volumeImbalance >= 0.3 && volumeImbalance <= 1) {
            return "POSITIVE(" + formattedVolumeImbalance + "), POSSIBLE BUY PRESSURE";

        } else {
            return "INVALID";
        }

    }

    private String volumeImbalanceIndicator(SimpleAlgoState state) {
        return analyzedVolumeImbalance(calculateVolumeImbalance(state));
    }


    private void childOrderLog(SimpleAlgoState state) {
        final var activeChildOrders = state.getActiveChildOrders();
        final var pendingBuyState = activeChildOrders.stream().filter(order -> order.getState()==OrderState.PENDING && order.getSide()==Side.BUY).toList();
        final var pendingSellState = activeChildOrders.stream().filter(order -> order.getState()==OrderState.PENDING && order.getSide()==Side.SELL).toList();
        final var filledSellState =activeChildOrders.stream().filter(order -> order.getState()==OrderState.FILLED && order.getSide()==Side.SELL).toList();
        final var cancelledState = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED).toList();
        logger.info("[MYALGO] Child Order Log Count: Filled Buy=" + filledBuyStateSize(state)+ ", Filled Sell=" + filledSellState.size()+", Pending Buy=" + pendingBuyState.size() + ", Pending Sell=" + pendingSellState.size() +", Cancelled=" + cancelledState.size());
    }

    private int filledBuyStateSize(SimpleAlgoState state) {
        return state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.BUY).toList().size();

    }

    private long calculateSharesRemainingToSell(SimpleAlgoState state) {
        long filledBuyVolume = state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.BUY).mapToLong(ChildOrder::getFilledQuantity).sum();
        long filledSellVolume = state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.SELL).mapToLong(ChildOrder::getFilledQuantity).sum();
        return filledSellVolume == 0 ? filledBuyVolume : filledBuyVolume - filledSellVolume;

    }

    protected long sellVolumeInline(SimpleAlgoState state, double participationRate) {

        double adjustedParticipationRate = participationRate;//allows participation rate to dynamically change depending on bid-ask spread(base participation rate = 25%)

        if (bidAskSpreadPercentage(state) > 3.0) {
            adjustedParticipationRate *= 0.80;// in a slightly more volatile spread the participation rate will be lowered from 25% to 20%
        } else if (bidAskSpreadPercentage(state) < 1.5) {
            adjustedParticipationRate *= 1.2; // in a slightly more liquid spread the participation rate will be increased from 25% to 30%
        }

        double averageFilledBuySize = state.getActiveChildOrders().stream().filter(order->order.getState()==OrderState.FILLED&&order.getSide()==Side.BUY).mapToDouble(ChildOrder::getFilledQuantity).average().orElse(0);
        long volumeInlineSellQuantity = (long) (averageFilledBuySize * (adjustedParticipationRate/100));
        return Math.min(volumeInlineSellQuantity, calculateSharesRemainingToSell(state));
      // in the case where there are not enough shares to trade at the calculated volume inline, the quantity of the remaining shares will be returned.
    }

    private void profitTracker(SimpleAlgoState state) {
        final var activeChildOrders = state.getActiveChildOrders();
        long filledBuyVolume = activeChildOrders.stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.BUY).mapToLong(ChildOrder::getFilledQuantity).sum();
        long filledSellVolume = activeChildOrders.stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.SELL).mapToLong(ChildOrder::getFilledQuantity).sum();
        double totalBuyPriceQuantity =  activeChildOrders.stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.BUY).mapToDouble(order -> order.getFilledQuantity() * order.getPrice()).sum();
        double totalSellPriceQuantity = activeChildOrders.stream().filter(order -> order.getState() == OrderState.FILLED && order.getSide() == Side.SELL).mapToDouble(order -> order.getFilledQuantity() * order.getPrice()).sum();
        double profit = totalSellPriceQuantity - totalBuyPriceQuantity;
        logger.info("[MYALGO] Revenue Tracker: Spent=£" + totalBuyPriceQuantity + ", Sold=£" + totalSellPriceQuantity + ", Profit=£" + profit + ", Shares Acquired=" +filledBuyVolume +  ", Shares Sold=" + filledSellVolume +  ", Shares Remaining=" +  calculateSharesRemainingToSell(state));
    }
}









