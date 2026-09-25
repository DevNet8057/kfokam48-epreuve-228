import { Navigate, Route, Routes } from "react-router-dom";
import { OuvertureSession } from "./pages/formateur/OuvertureSession";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/formateur" replace />} />
      <Route path="/formateur" element={<OuvertureSession />} />
    </Routes>
  );
}
