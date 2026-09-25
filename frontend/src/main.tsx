import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { ConfigProvider } from "antd";
import frFR from "antd/locale/fr_FR";
import { App } from "./App";
import { IdentiteProvider } from "./identite/IdentiteContext";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider locale={frFR}>
      <IdentiteProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </IdentiteProvider>
    </ConfigProvider>
  </React.StrictMode>
);
