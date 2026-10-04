import pages from "./page";
import support from "./support";
import repositories from "./repositories";
import { NavGroupData } from "../layout/Dashboard/Drawer/DrawerContent/Navigation/NavGroup";
import workspaces from "./workspaces";
import {
  BranchesOutlined,
  CloudServerOutlined,
  HddOutlined,
} from "@ant-design/icons";

const connectivity: NavGroupData = {
  id: "group-connectivity",
  title: "Connectivity",
  type: "group",
  children: [
    // {
    //   id: "maven",
    //   title: "Maven",
    //   type: "item",
    //   route: "/connectivity/maven",
    //   icon: CloudServerOutlined,
    // },
    {
      id: "git",
      title: "Git",
      type: "item",
      route: "/connectivity/git",
      icon: BranchesOutlined,
    },
    // {
    //   id: "uploads",
    //   title: "Uploads",
    //   type: "item",
    //   route: "/connectivity/uploads",
    //   icon: HddOutlined,
    // },
  ],
};

const menuItems: { items: NavGroupData[] } = {
  items: [connectivity, workspaces, repositories, support, pages],
};

export default menuItems;
