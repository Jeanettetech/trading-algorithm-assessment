package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.junit.Test;
import codingblackfemales.sotw.ChildOrder;

import static junit.framework.TestCase.assertEquals;

/**
 * This test plugs together all of the infrastructure, including the order book (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data coming back from the order book.
 *
 * If you cross the srpead (i.e. you BUY an order with a price which is == or > askPrice()) you will match, and receive
 * a fill back into your order from the order book (visible from the algo in the childOrders of the state object.
 *
 * If you cancel the order your child order will show the order status as cancelled in the childOrders of the state object.
 *
 */
public class MyAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }


    @Test
    public void competitiveBuyOrderCreationTest() throws Exception {

        send(createTick());//initial tick
        send(createTick2());//these two ticks allow the algorithm to meet the two filled child order requirement

        SimpleAlgoState state = container.getState();
        final var childBuyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.BUY)).toList().size();
        final var filledStateCount = state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).toList().size();

        //ADD asserts when you have implemented your algo logic
        assertEquals("Filled child order size is 2",2,filledStateCount);//asserts that the algorithm has reached the two filled order requirement and can now make competitive orders
        assertEquals("Active child buy order count is 3",3,childBuyOrderCount);//asserts the child buy order size is 3 before tick

        //when: the market has an imbalance and increased pressure on the sell side a child buy order is created if the best ask is greater than or equal to the VWAP benchmark

        send(createTick3());//this tick tests the algorithms logic and reaction to volume imbalance (best bid volume =100, best ask volume = 200)
        final var newChildBuyOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.BUY)).toList().size();

        assertEquals("Active child buy order count has increased to 4",4,newChildBuyOrderCount);//asserts active child buy orders have increased to 4

    }

    @Test
    public void competitiveSellOrderCreationTest() throws Exception {

        send(createTick());//initial tick
        send(createTick2());//these two ticks allow the algorithm to meet the minimum two filled child order requirement so any new orders will be created competitively.

        SimpleAlgoState state = container.getState();
        final var childSellOrderCount =  state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.SELL)).toList().size();
        final var filledStateCount = state.getActiveChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).toList().size();

        //ADD asserts when you have implemented your algo logic
        assertEquals("Filled order size is 2",2,filledStateCount);//asserts that the algorithm has reached the two filled order requirement and can now make competitive orders
        assertEquals("Active child sell order count is 0",0, childSellOrderCount);//asserts the child sell order size is 0 before tick

        //when: the market has an imbalance with increased pressure on the buy side a sell order is created if the best bid is less or equal to the VWAP benchmark

        send(createTick3());
        send(createTick4());//this tick tests the algorithms logic and reaction to volume imbalance (best bid volume =500, best ask volume = 46)
        final var newChildSellOrderCount = state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.SELL)).toList().size();
        assertEquals("Active child sell order count has increased to 6",6, newChildSellOrderCount);//asserts child sell orders have increased to 6

    }
    @Test
    public void  childBuyOrderCancellationTest() throws Exception {

        SimpleAlgoState state = container.getState();

        final var cancelledChildBuyOrdersCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED && order.getSide() == Side.BUY).count();

        //ADD asserts when you have implemented your algo logic
        assertEquals("Cancelled child buy order count is 0",0, cancelledChildBuyOrdersCount);//asserts that the cancelled buy order count is 0 before new tick

        //when: the market is indicating volatility due to the bid ask spread going over 4.5%. orders must be cancelled
        send(createTick());
        send(createTick2());// these ticks simulate a volatile market in which my algorithm dictates that non-viable orders should be cancelled to ensure best prices.
        send(createTick5());
        final var newCancelledChildBuyOrderCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED && order.getSide() == Side.BUY).count();
        //then: get the state
        assertEquals("Cancelled child buy order count has increased to 1 ",1, newCancelledChildBuyOrderCount);//asserts that the cancelled buy order count is 1 after new tick

    }
    @Test
    public void  childSellOrderCancellationTest() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());


        SimpleAlgoState state = container.getState();

        final var cancelledChildSellOrdersCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED && order.getSide() == Side.SELL).count();

        //ADD asserts when you have implemented your algo logic
        assertEquals("Cancelled child sell order count is 0",0,cancelledChildSellOrdersCount);//asserts that the cancelled sell order count is 0 before new tick

        //when: the market  indicates volatility due to the bid ask spread going over 4.5%.  orders must be cancelled to ensure the best prices are attained

        send(createTick5());// the ticket simulates a volatile market in which my algorithm dictates that non-viable orders should be cancelled to ensure best prices.

        final var newCancelledChildSellOrderCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED && order.getSide() == Side.SELL).count();
        //then: get the state
        assertEquals("Cancelled child sell order count has increased to 1",1, newCancelledChildSellOrderCount);//asserts that the cancelled sell order count is 1 after new tick

    }

    @Test
    public void filledStateQuantityUpdateTest() throws Exception {
        //create a sample market data tick....
        send(createTick());//initial ticks
        send(createTick2());
        send(createTick3());

        SimpleAlgoState state = container.getState();
        final var filledStateChildOrderCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).toList().size();
        final var filledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();


        //ADD asserts when you have implemented your algo logic
        assertEquals("Filled state order count is 2",2,filledStateChildOrderCount);//asserts that the algorithm has reached  two filled orders
        assertEquals("Filled order quantity is 602 ",602,filledQuantity);//asserts the filled quantity  is 602 before new ticks

        //when: fills are achieved child orders to need to be updated to reflect this


        //these ticks simulate conditions for orders to fill
        send(createTick4());
        send(createTick5());
        final var newFilledStateChildOrderCount = state.getChildOrders().stream().filter(order -> order.getState() == OrderState.FILLED).toList().size();
        final var newFilledQuantity = state.getChildOrders().stream().mapToLong(ChildOrder::getFilledQuantity).sum();

        assertEquals("New filled state order count is 4",  4,newFilledStateChildOrderCount);//asserts that filled state count is updated to 4 when orders are filled
        assertEquals("New filled order quantity is 712 ",712,newFilledQuantity);//asserts the filled quantity is updated to reflect the volume of the new fill orders


    }

//Unit Tests


    @Test
    public void ChildOrderSize() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(13,state.getChildOrders().size());//asserting that 13 Child Orders have been created
    }

    @Test
    public void activeChildOrderSize() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(11,state.getActiveChildOrders().size()); //asserts that 11 child orders exist after ticks were sent
    }
    @Test
    public void VWAPCalculation() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();//instance of algo logic to call the test on
        double calculatedVWAP = algoLogic.calculateVWAP(state); // Capturing result of the calculation

        // then

        assertEquals("VWAP calculation matches expected",97.82022471910112, calculatedVWAP);

    }

    @Test
    public void volumeImbalanceIndicator() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();//instance of algo logic to call the test on
        double calculatedVolumeImbalanceIndication = algoLogic.calculateVolumeImbalance(state); // Capturing result of the calculation

        // then
        assertEquals("Volume Imbalance calculation matches expected",0.25,calculatedVolumeImbalanceIndication);

    }
    @Test
    public void inlineSellVolumeCalculation() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();//instance of algo logic to call the test on
        long calculatedSellInlineVolume = algoLogic.sellVolumeInline(state,25.0); // Capturing result of the calculation

        // then
        assertEquals("Inline Sell Volume calculation matches expected",35, calculatedSellInlineVolume);

    }
    @Test
    public void buyChildOrderSize() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(4, state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.BUY)).toList().size()); //asserts that 4 child buy orders exist after ticks were sent
    }
    @Test
    public void sellChildOrderSize() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(7,state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.SELL)).toList().size());  //asserts that 7 child sell orders exist after ticks were sent

    }
    @Test
    public void cancelledChildOrderSize() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals( 2,state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED).toList().size() ); //asserts that 2 child orders were cancelled after ticks were sent

    }
}






