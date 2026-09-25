import { Route, Routes } from "react-router-dom";
import { Layout } from "./Layout";
import { ChoixIdentite } from "./pages/identite/ChoixIdentite";
import { EspaceFormateur } from "./pages/formateur/EspaceFormateur";
import { EspaceEtudiant } from "./pages/etudiant/EspaceEtudiant";
import { EspaceRelecteur } from "./pages/relecteur/EspaceRelecteur";
import { RouteProtegee } from "./identite/RouteProtegee";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<ChoixIdentite />} />
        <Route path="/formateur" element={<EspaceFormateur />} />
        <Route
          path="/etudiant"
          element={
            <RouteProtegee profilRequis="ETUDIANT">
              <EspaceEtudiant />
            </RouteProtegee>
          }
        />
        <Route
          path="/relecteur"
          element={
            <RouteProtegee profilRequis="RELECTEUR">
              <EspaceRelecteur />
            </RouteProtegee>
          }
        />
      </Route>
    </Routes>
  );
}
