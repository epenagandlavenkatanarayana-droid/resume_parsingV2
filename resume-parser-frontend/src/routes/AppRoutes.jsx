import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import HrLayout from '../layouts/HrLayout';

// Auth Pages
import Login from '../pages/auth/Login';

// HR Pages
import HrDashboard from '../pages/hr/HrDashboard';

// Candidate Pages
import UploadResume from '../pages/candidate/UploadResume';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Navigate to="/upload-resume" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/upload-resume" element={<UploadResume />} />

      {/* HR Routes */}
      <Route 
        path="/hr" 
        element={
          <ProtectedRoute roleRequired={['HR', 'ADMIN']}>
            <HrLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/hr/dashboard" replace />} />
        <Route path="dashboard" element={<HrDashboard />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/upload-resume" replace />} />
    </Routes>
  );
};

export default AppRoutes;
