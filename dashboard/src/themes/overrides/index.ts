// third-party
import { merge } from 'lodash-es';

// project imports
import Badge from './Badge.js';
import Button from './Button.js';
import ButtonBase from './ButtonBase.js';
import CardContent from './CardContent.js';
import Checkbox from './Checkbox.js';
import Chip from './Chip.js';
import Drawer from './Drawer.js';
import FormHelperText from './FormHelperText.js';
import IconButton from './IconButton.js';
import InputLabel from './InputLabel.js';
import LinearProgress from './LinearProgress';
import Link from './Link';
import ListItemButton from './ListItemButton';
import ListItemIcon from './ListItemIcon';
import OutlinedInput from './OutlinedInput';
import Tab from './Tab';
import TableBody from './TableBody';
import TableCell from './TableCell';
import TableHead from './TableHead';
import TableRow from './TableRow';
import Tabs from './Tabs';
import Tooltip from './Tooltip';
import Typography from './Typography';
import {Theme} from "@mui/material/styles";

// ==============================|| OVERRIDES - MAIN ||============================== //

export default function ComponentsOverrides(theme: Theme) {
  return merge(
    Badge(theme),
    Button(theme),
    ButtonBase(),
    CardContent(),
    Checkbox(theme),
    Chip(theme),
    Drawer(),
    FormHelperText(),
    IconButton(theme),
    InputLabel(theme),
    LinearProgress(),
    Link(),
    ListItemButton(theme),
    ListItemIcon(theme),
    OutlinedInput(theme),
    Tab(theme),
    TableBody(theme),
    TableCell(theme),
    TableHead(theme),
    TableRow(),
    Tabs(),
    Tooltip(theme),
    Typography()
  );
}
