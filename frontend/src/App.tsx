import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./Layout";
import { OuvertureSession } from "./pages/formateur/OuvertureSession";
import { EspaceEtudiant } from "./pages/etudiant/EspaceEtudiant";
import { EspaceRelecteur } from "./pages/relecteur/EspaceRelecteur";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/formateur" replace />} />
        <Route path="/formateur" element={<OuvertureSession />} />
        <Route path="/etudiant" element={<EspaceEtudiant />} />
        <Route path="/relecteur" element={<EspaceRelecteur />} />
      </Route>
    </Routes>
  );
}
