
// project imports
import DrawerHeaderStyled from './DrawerHeaderStyled.js';
import Logo from '../../../../components/logo';

// ==============================|| DRAWER HEADER ||============================== //

export default function DrawerHeader({ open }: { open: boolean }) {

  return (
    <DrawerHeaderStyled
      // @ts-expect-error: 'open' is used internally. Was converted from JS.
      open={open}
      sx={{
        minHeight: '60px',
        width: 'initial',
        paddingTop: '8px',
        paddingBottom: '8px',
        paddingLeft: open ? '24px' : 0
      }}
    >
      <Logo isIcon={!open} sx={{ width: open ? 'auto' : 35, height: 35 }} />
    </DrawerHeaderStyled>
  );
}
