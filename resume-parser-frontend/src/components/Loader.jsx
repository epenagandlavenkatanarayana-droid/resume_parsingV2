
import { BiLoaderAlt } from 'react-icons/bi';

const Loader = ({ fullScreen = false, message = "Loading..." }) => {
  const containerClass = fullScreen
    ? "fixed inset-0 flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm z-50"
    : "flex flex-col items-center justify-center p-8 w-full";

  return (
    <div className={containerClass}>
      <BiLoaderAlt className="animate-spin text-primary text-4xl mb-4" />
      <p className="text-text-light font-medium">{message}</p>
    </div>
  );
};

export default Loader;
