import { useSelector } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";

function ProtectedRoute({children}) {
    const isLoggedIn = useSelector((state) => state.user.isLoggedIn);
    const location = useLocation();

    if(!isLoggedIn && location.pathname != '/') {
        return <Navigate to="/" replace />;
    }

        return children;
    }

export default ProtectedRoute;