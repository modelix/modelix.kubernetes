import { Link } from 'react-router-dom';
import ButtonBase from '@mui/material/ButtonBase';
import Logo from './LogoMain';
import LogoIcon from './LogoIcon';
import {SxProps} from "@mui/system";
import {Theme} from "@mui/material/styles";
import {Path} from "react-router";

// ==============================|| MAIN LOGO ||============================== //

export default function LogoSection({ isIcon, sx, to }: { isIcon: boolean; sx: SxProps<Theme>; to?: string | Partial<Path> }) {
  return (
    <ButtonBase disableRipple component={Link} to={to || "/"} sx={sx}>
      {isIcon ? <LogoIcon /> : <Logo />}
    </ButtonBase>
  );
}
