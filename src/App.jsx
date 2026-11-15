import { Routes, Route } from "react-router-dom";
import Landing from "./pages/Landing";
import Admin from "./pages/Admin";
import NotFound from "./pages/NotFound";
import ScanPage from "./pages/ScanPage";
import StaffPage from "./pages/StaffPage";
import OwnerPage from "./pages/OwnerPage";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/admin" element={<Admin />} />
      <Route path="/s/:code" element={<ScanPage />} />
      <Route path="/staff" element={<StaffPage />} />
      <Route path="/owner" element={<OwnerPage />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default App;
