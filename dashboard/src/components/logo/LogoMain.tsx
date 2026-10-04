// material-ui
import { useTheme } from '@mui/material/styles';

/**
 * if you want to use image instead of <svg> uncomment following.
 *
 * import logoDark from 'assets/images/logo-dark.svg';
 * import logo from 'assets/images/logo.svg';
 *
 */

// ==============================|| LOGO SVG ||============================== //

export default function LogoMain() {
  const theme = useTheme();
  return (
    <>
      <svg height="35" viewBox="0 0 1600 320" fill="none" xmlns="http://www.w3.org/2000/svg">
        <g transform="translate(-53, -98)">
          <g transform="scale(0.6, 0.6) translate(100, 180)">
            <g transform="translate(-20, 0)">
              <line fill="none" stroke="#888" x1="125" y1="150" x2="275" y2="50" strokeWidth="25"/>
              <line fill="none" stroke="#888" x1="275" y1="50" x2="225" y2="150" strokeWidth="25"/>
              <line fill="none" stroke="#888" x1="275" y1="50" x2="340" y2="250" strokeWidth="25"/>
              <line fill="none" stroke="#888" x1="460" y1="250" x2="275" y2="50" strokeWidth="25"/>
              <ellipse fill="#888" stroke="#888" strokeWidth="8" cx="275" cy="50" rx="25" ry="25"/>
              <ellipse fill="#00457c" stroke="#00457c" strokeWidth="8" cx="75" cy="350" rx="25" ry="25"/>
              <ellipse fill="#00457c" stroke="#00457c" strokeWidth="8" cx="175" cy="350" rx="25" ry="25"/>
              <ellipse fill="#00457c" stroke="#00457c" strokeWidth="8" cx="275" cy="350" rx="25" ry="25"/>
              <ellipse fill="#00457c" stroke="#00457c" strokeWidth="8" cx="125" cy="150" rx="25" ry="25"/>
              <ellipse fill="#00457c" stroke="#00457c" strokeWidth="8" cx="225" cy="150" rx="25" ry="25"/>
              <line fill="none" stroke="#00457c" x1="75" y1="350" x2="125" y2="150" strokeWidth="25"/>
              <line fill="none" stroke="#00457c" strokeWidth="25" x1="125" y1="150" x2="175" y2="350"/>
              <line fill="none" stroke="#00457c" strokeWidth="25" x1="225" y1="150" x2="175" y2="350"/>
              <line fill="none" stroke="#00457c" strokeWidth="25" x1="275" y1="350" x2="225" y2="150"/>
              <ellipse fill="#000" stroke="#000" strokeWidth="8" cx="340" cy="250" rx="25" ry="25"/>
              <ellipse fill="#000" stroke="#000" strokeWidth="8" cx="340" cy="450" rx="25" ry="25"/>
              <ellipse fill="#000" stroke="#000" strokeWidth="8" cx="400" cy="350" rx="25" ry="25"/>
              <ellipse fill="#000" stroke="#000" strokeWidth="8" cx="460" cy="250" rx="25" ry="25"/>
              <ellipse fill="#000" stroke="#000" strokeWidth="8" cx="460" cy="450" rx="25" ry="25"/>
              <line fill="none" stroke="#000" x1="340" y1="250" x2="400" y2="350" strokeWidth="25"/>
              <line fill="none" stroke="#000" x1="460" y1="250" x2="400" y2="350" strokeWidth="25"/>
              <line fill="none" stroke="#000" x1="340" y1="450" x2="400" y2="350" strokeWidth="25"/>
              <line fill="none" stroke="#000" x1="460" y1="450" x2="400" y2="350" strokeWidth="25"/>
            </g>
          </g>
          <text fontFamily="Inter" fontWeight="700" fontSize="250" y="360" x="500" fill="#00457c">Modelix</text>
        </g>
      </svg>
    </>
  );
}
