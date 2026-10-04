// material-ui
import { useTheme } from '@mui/material/styles';
import Box from '@mui/material/Box';

// project imports
import getColors from '../../utils/getColors';

export default function Dot({ color, size, variant, sx }: { color?: string; size?: number; variant?: string; sx?: any }) {
  const theme = useTheme();
  const colors = getColors(theme, color || 'primary');
  const { main } = colors;

  return (
    <Box
      sx={{
        width: size || 8,
        height: size || 8,
        borderRadius: '50%',
        ...(variant === 'outlined' ? { border: `1px solid ${main}` } : { bgcolor: main }),
        ...sx
      }}
    />
  );
}
