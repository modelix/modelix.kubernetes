// assets
import { ChromeOutlined, QuestionOutlined } from '@ant-design/icons';
import {NavGroupData} from "../layout/Dashboard/Drawer/DrawerContent/Navigation/NavGroup.tsx";

// icons
const icons = {
  ChromeOutlined,
  QuestionOutlined
};

// ==============================|| MENU ITEMS - SAMPLE PAGE & DOCUMENTATION ||============================== //

const support: NavGroupData = {
  id: 'support',
  title: 'Support',
  type: 'group',
  children: [
    {
      id: 'documentation',
      title: 'Documentation',
      type: 'item',
      url: 'https://docs.modelix.org/',
      icon: QuestionOutlined,
      target: true
    }
  ]
};

export default support;
