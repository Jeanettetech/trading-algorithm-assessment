import { MarketDepthRow } from "./useMarketDepthData"; //type definition
import "./MarketDepthPanel.css";
import { PriceCell } from "./PriceCell"; //price render component

interface MarketDepthPanelProps {
  data: MarketDepthRow[]; //defines the prop expected for this component
}

export const MarketDepthPanel = ({ data }: MarketDepthPanelProps) => {
  //deconstruct prop

  const quantities = data.map((row) => {
    return Math.max(row.bidQuantity, row.offerQuantity);
  }); //maps over row returning the maximum then store in quantity array(row)

  const max = Math.max(...quantities); //finds maximum value across the rows and stores in max variable

  const percentageQuantity = (quantity: number, max: number) =>
    (quantity / max) * 100; //function to calculate percentage relative to the max

  return (
    <>
      <h2>Algo Trading Price Ticker</h2>
      <div className="table-container">
        <table className="MarketDepthPanel">
          <thead>
            <tr>
              <th></th>
              <th colSpan={3}>Bid</th>
              <th colSpan={3}>Ask</th>
            </tr>
            <tr>
              <th></th>
              <th>Quantity</th>
              <th className="percentage-header">Bid % Change</th>
              <th className="price-header bid">Bid Price</th>
              <th className="price-header ask">Ask Price</th>
              <th className="percentage-header">Ask % Change</th>
              <th>Quantity</th>
            </tr>
          </thead>
          <tbody>
            {data.map(
              //table body populated dynamically using data.map, each row maps to a new tr
              (
                row,
                index //maps over data array new  for each entry
              ) => (
                <tr key={index}>
                  {/*key for unique identification for each row*/}
                  <td>{index}</td>
                  <td className="bid">
                    <div className="gauge-container">
                      <div
                        className="gauge-bar"
                        style={{
                          width: `${percentageQuantity(row.bidQuantity, max)}%`, //template literal to embed expression into string directly
                        }}
                      ></div>
                      <span>{row.bidQuantity}</span>{" "}
                      {/*bid quantity displayed in span element */}
                    </div>
                  </td>
                  <PriceCell
                    className="percentage-cell bid"
                    price={row.bid}
                    isBid={true}
                    showPercentage={true}
                  />
                  <PriceCell
                    price={row.bid}
                    isBid={true}
                    showPercentage={false}
                  />
                  <PriceCell
                    price={row.offer}
                    isBid={false}
                    showPercentage={false}
                  />
                  <PriceCell
                    className="percentage-cell ask"
                    price={row.offer}
                    isBid={false}
                    showPercentage={true}
                  />
                  <td className="ask">
                    <div className="gauge-container">
                      <div
                        className="gauge-bar"
                        style={{
                          width: `${percentageQuantity(
                            row.offerQuantity,
                            max
                          )}%`,
                        }}
                      ></div>
                      <span>{row.offerQuantity}</span>
                      {/*ask quantity displayed in span element */}
                    </div>
                  </td>
                </tr>
              )
            )}
          </tbody>
        </table>
      </div>
    </>
  );
};
