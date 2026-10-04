import { DashboardOutlined } from '@ant-design/icons';
import {NavGroupData} from "../layout/Dashboard/Drawer/DrawerContent/Navigation/NavGroup.tsx";

const dashboard: NavGroupData = {
  id: 'group-dashboard',
  title: 'Navigation',
  type: 'group',
  children: [
    {
      id: 'dashboard',
      title: 'Dashboard',
      type: 'item',
      url: '/dashboard/default',
      icon: DashboardOutlined,
      breadcrumbs: false
    }
  ]
};

export default dashboard;
