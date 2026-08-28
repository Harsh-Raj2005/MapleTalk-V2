import { useMutation, useQueryClient } from "@tanstack/react-query";
import { login } from "../lib/api";
import { setToken } from "../lib/token";

const useLogin = () => {
  const queryClient = useQueryClient();
  const { mutate, isPending, error } = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setToken(data.token);
      // We already have the authoritative user from this response —
      // seed the cache directly instead of firing a redundant /me call.
      queryClient.setQueryData(["authUser"], data.user);
    },
  });

  return { error, isPending, loginMutation: mutate };
};

export default useLogin;
