package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.order.Side;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


/**
 * This test is designed to check your algo behavior in isolation of the order book.
 *
 * You can tick in market data messages by creating new versions of createTick() (ex. createTick2, createTickMore etc..)
 *
 * You should then add behaviour to your algo to respond to that market data by creating or cancelling child orders.
 *
 * When you are comfortable you algo does what you expect, then you can move on to creating the MyAlgoBackTest.
 *
 */
public class MyAlgoTest extends AbstractAlgoTest {
    /*Due to bugs and the AbstractAlsoTest not being able to  test for filled orders which is an integral part of my algorithm
    I have moved the unit tests to MyAlgoBackTest
     */
    @Override
    public AlgoLogic createAlgoLogic() {
        //this adds your algo logic to the container classes
        return new MyAlgoLogic();
    }

    @Before
    public void setUp() throws Exception {
        send(createTick());
        send(createTick2());
        send(createTick3());
        send(createTick4());
        send(createTick5());


    }

    @Test
    public void ChildOrderSize() throws Exception {

        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(13,state.getChildOrders().size()); //11 is expected but an incorrect value of 21 is returned
    }

    @Test
    public void activeChildOrderSize() throws Exception {

        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(11,state.getActiveChildOrders().size());//11 is expected but an incorrect value of 21 is returned
    }
    @Test
    public void VWAPCalculation() throws Exception {

        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();//instance of algo logic to call the test on
        double calculatedVWAP = algoLogic.calculateVWAP(state); // Capturing result of the calculation

        // then

        assertEquals("VWAP calculation matches expected",97.82022471910112, calculatedVWAP);//97.82022471910112 is expected but an incorrect value of 0.0 is returned

    }

    @Test
    public void volumeImbalanceIndicator() throws Exception {

        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        double calculatedVolumeImbalanceIndication = algoLogic.calculateVolumeImbalance(state); // Capturing result of the calculation

        // then
        assertEquals("Volume Imbalance calculation matches expected",0.25,calculatedVolumeImbalanceIndication);//0.25 is expected but an incorrect value of 0.9607843137254902 is returned

    }
    @Test
    public void inlineSellVolumeCalculation() throws Exception {

        //when
        var state = container.getState(); // Ensuring state is retrieved
        MyAlgoLogic algoLogic = new MyAlgoLogic();//instance of algo logic to call the test on
        long calculatedSellInlineVolume = algoLogic.sellVolumeInline(state,25.0); // Capturing result of the calculation

        // then
        assertEquals("Inline Sell Volume calculation matches expected",35, calculatedSellInlineVolume);//35 is expected but an incorrect value of 0 is returned

    }
    @Test
    public void buyChildOrderSize() throws Exception {

        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(4, state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.BUY)).toList().size());//4 is expected but an incorrect value of 21 is returned
    }
    @Test
    public void sellChildOrderSize() throws Exception {

        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals(7,state.getActiveChildOrders().stream().filter(order -> order.getSide().equals(Side.SELL)).toList().size()); //7 is expected but an incorrect value of 0 is returned


    }
    @Test
    public void cancelledChildOrderSize() throws Exception {

        //when
        SimpleAlgoState state = container.getState();

        //then
        assertEquals( 2,state.getChildOrders().stream().filter(order -> order.getState() == OrderState.CANCELLED).toList().size());//2 is expected but an incorrect value of 0 is returned

    }
}
