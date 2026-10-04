// ==============================|| CUSTOM FUNCTION - COLORS ||============================== //

import {Theme} from "@mui/material/styles";
import {CustomPaletteColor} from "../themes/theme";

export default function getColors(theme: Theme, color: string): CustomPaletteColor {
  switch (color) {
    case 'secondary':
      return theme.palette.secondary as CustomPaletteColor;
    case 'error':
      return theme.palette.error as CustomPaletteColor;
    case 'warning':
      return theme.palette.warning as CustomPaletteColor;
    case 'info':
      return theme.palette.info as CustomPaletteColor;
    case 'success':
      return theme.palette.success as CustomPaletteColor;
    default:
      return theme.palette.primary as CustomPaletteColor;
  }
}
