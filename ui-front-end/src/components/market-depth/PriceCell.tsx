import { useRef, useEffect } from "react";
import "./PriceCell.css";
export interface PriceCellProps {
  price: number;
  isBid?: boolean;
  showPercentage?: boolean;
  className?: string;
  // defining interface
}

export const PriceCell = ({
  price,
  isBid,
  showPercentage,
  className,
}: PriceCellProps) => {
  const lastValueRef = useRef(price); //keepeing track of last price
  const diff = price - lastValueRef.current;
  const percentageDiff = (diff / lastValueRef.current) * 100; //calculating percentage difference

  useEffect(() => {
    lastValueRef.current = price;
  }, [price]); //effect hook triggers when price changes

  const color = diff > 0 ? "green" : diff < 0 ? "red" : "black";
  const formattedPrice = price.toFixed(2); // Format price to two decimal places//ternary operator fir colour

  return (
    <td className={`price-cell ${isBid ? "bid" : "offer"} ${className}`}>
      {/*template literal* bid /offer is referring the props data array*/}
      {showPercentage ? (
        <span
          className="percentage"
          style={{
            color,
          }}
        >
          ({percentageDiff.toFixed(2)}%)
        </span>
      ) : (
        <>
          {isBid ? (
            <span className="price-arrow" style={{ color }}>
              {/* span containsers for text*/}
              {diff > 0 ? "▲" : diff < 0 ? "▼" : null}
              {formattedPrice}
            </span>
          ) : (
            <span className="price-arrow" style={{ color }}>
              {formattedPrice}
              {diff > 0 ? "▲" : diff < 0 ? "▼" : null}
            </span>
          )}
        </>
      )}
    </td>
  );
};
