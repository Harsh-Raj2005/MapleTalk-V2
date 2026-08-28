import { useQueryClient } from "@tanstack/react-query";
import { clearToken } from "../lib/token";

// V2 is stateless (Bearer JWT, no server session) — there is no logout
// endpoint to call. Logging out is purely a client-side operation.
const useLogout = () => {
  const queryClient = useQueryClient();

  const logoutMutation = () => {
    clearToken();
    queryClient.setQueryData(["authUser"], null);
    queryClient.clear();
  };

  return { logoutMutation, isPending: false, error: null };
};
export default useLogout;
