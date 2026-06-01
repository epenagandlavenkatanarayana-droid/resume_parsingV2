import { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import Loader from './Loader';

const ProtectedRoute = ({ children, roleRequired }) => {
  const { user, isAuthenticated, loading } = useContext(AuthContext);

  if (loading) {
    return <Loader fullScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Support string or array of allowed roles
  const hasRequiredRole = () => {
    if (!roleRequired) return true;
    if (Array.isArray(roleRequired)) {
      return roleRequired.includes(user?.role);
    }
    return user?.role === roleRequired;
  };

  if (!hasRequiredRole()) {
    return <Navigate to="/upload-resume" replace />;
  }

  return children;
};

export default ProtectedRoute;
