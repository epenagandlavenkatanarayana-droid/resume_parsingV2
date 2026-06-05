import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import HrLayout from '../layouts/HrLayout';
import AdminLayout from '../layouts/AdminLayout';

import AdminDashboard from '../pages/admin/AdminDashboard';
import Login from '../pages/auth/Login';

// HR Pages
import HrDashboard from '../pages/hr/HrDashboard';
import Profile from '../pages/hr/Profile';

// Candidate Pages
import UploadResume from '../pages/candidate/UploadResume';
import OfferStatus from '../pages/candidate/OfferStatus';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Navigate to="/upload-resume" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/upload-resume" element={<UploadResume />} />
      <Route path="/offer-status" element={<OfferStatus />} />

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
        <Route path="profile" element={<Profile />} />
      </Route>

      {/* Admin Routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute roleRequired={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="dashboard" element={<AdminDashboard />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/upload-resume" replace />} />
    </Routes>
  );
};

export default AppRoutes;
