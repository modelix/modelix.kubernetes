import { Suspense } from 'react';

// project imports
import Loader from './Loader.js';

// ==============================|| LOADABLE - LAZY LOADING ||============================== //

const Loadable = (Component: any) => (props: any) => {
  return (
    <Suspense fallback={<Loader />}>
      <Component {...props} />
    </Suspense>
  );
};

export default Loadable;
