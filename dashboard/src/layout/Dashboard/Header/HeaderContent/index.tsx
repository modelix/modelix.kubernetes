// material-ui
import useMediaQuery from '@mui/material/useMediaQuery';
import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Box from '@mui/material/Box';

// project imports
import Search from './Search';
import Profile from './Profile/index';
import Notification from './Notification';
import MobileSection from './MobileSection';

// project import
import { GithubOutlined, DockerOutlined } from '@ant-design/icons';

// ==============================|| HEADER - CONTENT ||============================== //

export default function HeaderContent() {
  const downLG = useMediaQuery((theme) => theme.breakpoints.down('lg'));

  return (
    <>
      {!downLG && <Search />}
      {downLG && <Box sx={{ width: '100%', ml: 1 }} />}

      <Box sx={{ flexShrink: 0, ml: 0.75 }}>
        <IconButton
            component={Link}
            href="https://github.com/modelix"
            target="_blank"
            color="secondary"
            title="Modelix on GitHub"
            sx={(theme) => ({
              color: 'text.primary',
              bgcolor: 'transparent',
              ...theme.applyStyles('dark', { bgcolor: 'transparent' })
            })}
        >
          <GithubOutlined />
        </IconButton>
      </Box>

      <Box sx={{ flexShrink: 0, ml: 0.75 }}>
        <IconButton
            component={Link}
            href="https://hub.docker.com/u/modelix"
            target="_blank"
            color="secondary"
            title="Modelix on Docker Hub"
            sx={(theme) => ({
              color: 'text.primary',
              bgcolor: 'transparent',
              ...theme.applyStyles('dark', { bgcolor: 'transparent' })
            })}
        >
          <DockerOutlined />
        </IconButton>
      </Box>

      {/*<Notification />*/}
      {!downLG && <Profile />}
      {downLG && <MobileSection />}
    </>
  );
}
