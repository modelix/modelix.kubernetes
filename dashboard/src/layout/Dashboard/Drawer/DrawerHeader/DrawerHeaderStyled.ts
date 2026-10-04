// material-ui
import {styled, Theme as MaterialTheme} from '@mui/material/styles';
import Box from '@mui/material/Box';

// ==============================|| DRAWER HEADER - STYLED ||============================== //

const DrawerHeaderStyled = styled(Box, { shouldForwardProp: (prop) => prop !== 'open' })(({ theme }) => ({
  ...theme.mixins.toolbar,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  paddingLeft: theme.spacing(0),
  variants: [
    {
      props: ({ open }: any) => open,
      style: { justifyContent: 'flex-start', paddingLeft: theme.spacing(3) }
    }
  ]
}));

export default DrawerHeaderStyled;
