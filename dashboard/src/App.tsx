import ScrollTop from "./components/ScrollTop";

import ThemeCustomization from "./themes";
import { BrowserRouter, Route, Routes, useParams } from "react-router";
import MavenConnectivityPage from "./pages/connectivity/maven.tsx";
import DashboardLayout from "./layout/Dashboard";
import GitConnectivityPage from "./pages/connectivity/git/git.tsx";
import UploadsConnectivityPage from "./pages/connectivity/uploads.tsx";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Typography from "@mui/material/Typography";
import { useAuth } from "react-oidc-context";
import WorkspacesList, {SingleWorkspace} from "./pages/workspaces/workspaces.tsx";
import {Fragment} from "react";
import Button from "@mui/material/Button";
import LoginOutlined from "@ant-design/icons/LoginOutlined";
import Box from "@mui/material/Box";

const queryClient = new QueryClient();

export default function App() {
  const auth = useAuth();

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeCustomization>
        <ScrollTop>
          <BrowserRouter basename={import.meta.env.BASE_URL}>
            <Routes>
              <Route path="/" element={<DashboardLayout />}>
                <Route index element={<div>Home</div>} />
                <Route path="connectivity">
                  <Route path="maven" element={<RequiresLogin><MavenConnectivityPage /></RequiresLogin>} />
                  <Route path="git" element={<RequiresLogin><GitConnectivityPage /></RequiresLogin>} />
                  <Route path="uploads" element={<RequiresLogin><UploadsConnectivityPage /></RequiresLogin>} />
                </Route>
                <Route path="workspaces">
                  <Route path="workspaces">
                    <Route index element={<RequiresLogin><WorkspacesList /></RequiresLogin>} />
                    <Route path=":workspaceId" element={<RequiresLogin><SingleWorkspacePage /></RequiresLogin>}/>
                  </Route>
                  <Route path="instances">
                    <Route path=":instanceId">
                      <Route path="mps">
                        <Route index element={<InstanceMpsUI />} />
                      </Route>
                    </Route>
                  </Route>
                </Route>
                <Route path="repositories">
                  <Route index element={<Typography>Repositories</Typography>} />
                  <Route path=":repositoryId" element={<Typography>Repository</Typography>}/>
                </Route>
                <Route
                  path="jwt"
                  element={
                    <Typography>
                      {auth.user?.access_token ?? auth.user?.id_token}
                    </Typography>
                  }
                />
              </Route>
            </Routes>
          </BrowserRouter>
        </ScrollTop>
      </ThemeCustomization>
    </QueryClientProvider>
  );
}

function SingleWorkspacePage() {
  const params = useParams()
  return <SingleWorkspace workspaceId={params.workspaceId!} />
}

function InstanceMpsUI() {
  const { instanceId } = useParams();
  return <iframe src="https://modelix.org/"></iframe>;
}

function RequiresLogin({children}: {children: any}) {
  const auth = useAuth();
  if (auth.isAuthenticated) {
    return <Fragment>{children}</Fragment>
  } else {
    return (
        <Box>
          <Button variant="contained" startIcon={<LoginOutlined/>} onClick={() => auth.signinPopup()} sx={{ml: 3}}>
            Login
          </Button>
        </Box>
    );
  }
}