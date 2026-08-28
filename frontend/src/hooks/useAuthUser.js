import { useQuery } from "@tanstack/react-query";
import { getAuthUser } from "../lib/api";
import { getToken } from "../lib/token";

const useAuthUser = () => {
  const authUser = useQuery({
    queryKey: ["authUser"],
    queryFn: getAuthUser,
    // No token stored means there is nothing to restore — skip the request
    // entirely rather than firing a request that can only 401.
    enabled: Boolean(getToken()),
    retry: false,
  });

  return { isLoading: Boolean(getToken()) && authUser.isLoading, authUser: authUser.data };
};
export default useAuthUser;
