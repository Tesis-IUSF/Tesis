import { useState } from "react";
import { Outlet } from "react-router-dom";
import Topbar from "./Topbar";
import Sidebar from "./Sidebar";
import "./Layout.css";

const CLAVE_COLAPSADO = "menuColapsado";

function leerColapsado() {
  try {
    return localStorage.getItem(CLAVE_COLAPSADO) === "1";
  } catch {
    return false;
  }
}

function esMovil() {
  return window.matchMedia("(max-width: 768px)").matches;
}

function Layout() {
  const [colapsado, setColapsado] = useState(leerColapsado);
  const [abiertoMovil, setAbiertoMovil] = useState(false);

  const alternarMenu = () => {
    if (esMovil()) {
      setAbiertoMovil((abierto) => !abierto);
      return;
    }
    const siguiente = !colapsado;
    setColapsado(siguiente);
    try {
      localStorage.setItem(CLAVE_COLAPSADO, siguiente ? "1" : "0");
    } catch {
      // sin almacenamiento: el menú funciona igual, solo no se recuerda
    }
  };

  return (
    <div className="layout">
      <Topbar onToggleMenu={alternarMenu} />
      <div className="layout-cuerpo">
        <Sidebar
          colapsado={colapsado}
          abiertoMovil={abiertoMovil}
          onCerrar={() => setAbiertoMovil(false)}
        />
        <main className="layout-contenido">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default Layout;
