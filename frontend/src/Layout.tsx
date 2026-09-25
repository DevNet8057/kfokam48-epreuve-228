import { Layout as AntLayout, Menu } from "antd";
import { Link, Outlet, useLocation } from "react-router-dom";

const { Header, Content } = AntLayout;

const ESPACES = [
  { key: "/formateur", label: "Formateur" },
  { key: "/etudiant", label: "Étudiant" },
  { key: "/relecteur", label: "Relecteur" },
];

/**
 * F2 : les trois espaces (formateur, étudiant, relecteur) sont accessibles depuis une navigation commune.
 */
export function Layout() {
  const location = useLocation();

  return (
    <AntLayout style={{ minHeight: "100vh" }}>
      <Header style={{ display: "flex", alignItems: "center" }}>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          items={ESPACES.map((espace) => ({
            key: espace.key,
            label: <Link to={espace.key}>{espace.label}</Link>,
          }))}
          style={{ flex: 1, minWidth: 0 }}
        />
      </Header>
      <Content style={{ padding: "16px" }}>
        <Outlet />
      </Content>
    </AntLayout>
  );
}
