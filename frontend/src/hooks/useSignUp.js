import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signup } from "../lib/api";
import { setToken } from "../lib/token";

const useSignUp = () => {
  const queryClient = useQueryClient();

  const { mutate, isPending, error } = useMutation({
    mutationFn: signup,
    onSuccess: (data) => {
      setToken(data.token);
      queryClient.setQueryData(["authUser"], data.user);
    },
  });
  return { isPending, error, signupMutation: mutate };
};
export default useSignUp;
