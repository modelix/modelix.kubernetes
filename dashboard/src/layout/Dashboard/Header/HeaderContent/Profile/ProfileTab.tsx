
// material-ui
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';

// assets
import EditOutlined from '@ant-design/icons/EditOutlined';
import ProfileOutlined from '@ant-design/icons/ProfileOutlined';
import LogoutOutlined from '@ant-design/icons/LogoutOutlined';
import LoginOutlined from '@ant-design/icons/LoginOutlined';
import UserOutlined from '@ant-design/icons/UserOutlined';
import WalletOutlined from '@ant-design/icons/WalletOutlined';
import {AuthContextProps, useAuth} from "react-oidc-context";

// ==============================|| HEADER PROFILE - PROFILE TAB ||============================== //

export default function ProfileTab() {
  const auth = useAuth();
  const accountUrl = `https://${window.location.host}/realms/modelix/account/`
  return (
    <List component="nav" sx={{ p: 0, '& .MuiListItemIcon-root': { minWidth: 32 } }}>
      <ListItemButton onClick={() => window.open(accountUrl, "_blank")}>
        <ListItemIcon>
          <UserOutlined />
        </ListItemIcon>
        <ListItemText primary="View Profile" />
      </ListItemButton>
      {
        auth.isAuthenticated ? (
            <ListItemButton onClick={() => auth.signoutPopup()}>
              <ListItemIcon>
                <LogoutOutlined />
              </ListItemIcon>
              <ListItemText primary="Logout" />
            </ListItemButton>
        ) : (
            <ListItemButton onClick={() => auth.signinPopup()}>
              <ListItemIcon>
                <LoginOutlined />
              </ListItemIcon>
              <ListItemText primary="Login" />
            </ListItemButton>
        )
      }
    </List>
  );
}
