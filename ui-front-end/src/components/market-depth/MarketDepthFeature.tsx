import { useMarketDepthData } from "./useMarketDepthData";
import { schemas } from "../../data/algo-schemas";
import { MarketDepthPanel } from "./MarketDepthPanel";

export const MarketDepthFeature = () => {
  const data = useMarketDepthData(schemas.prices);

  return <MarketDepthPanel data={data} />;
};
