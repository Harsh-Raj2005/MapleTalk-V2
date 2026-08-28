import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { acceptFriendRequest, getFriendRequests, rejectFriendRequest } from "../lib/api";
import { UserCheckIcon, XIcon } from "lucide-react";
import toast from "react-hot-toast";
import NoNotificationsFound from "../components/NoNotificationsFound";

const NotificationsPage = () => {
  const queryClient = useQueryClient();

  const { data: incomingRequests = [], isLoading } = useQuery({
    queryKey: ["friendRequests"],
    queryFn: getFriendRequests,
  });

  const invalidateRequestQueries = () => {
    queryClient.invalidateQueries({ queryKey: ["friendRequests"] });
    queryClient.invalidateQueries({ queryKey: ["friends"] });
  };

  const { mutate: acceptRequestMutation, isPending: isAccepting } = useMutation({
    mutationFn: acceptFriendRequest,
    onSuccess: invalidateRequestQueries,
    onError: (error) => toast.error(error.response?.data?.message || "Could not accept request"),
  });

  const { mutate: rejectRequestMutation, isPending: isRejecting } = useMutation({
    mutationFn: rejectFriendRequest,
    onSuccess: invalidateRequestQueries,
    onError: (error) => toast.error(error.response?.data?.message || "Could not reject request"),
  });

  const isBusy = isAccepting || isRejecting;

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="container mx-auto max-w-4xl space-y-8">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight mb-6">Notifications</h1>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <span className="loading loading-spinner loading-lg"></span>
          </div>
        ) : incomingRequests.length > 0 ? (
          <section className="space-y-4">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <UserCheckIcon className="h-5 w-5 text-primary" />
              Friend Requests
              <span className="badge badge-primary ml-2">{incomingRequests.length}</span>
            </h2>

            <div className="space-y-3">
              {incomingRequests.map((request) => (
                <div
                  key={request.id}
                  className="card bg-base-200 shadow-sm hover:shadow-md transition-shadow"
                >
                  <div className="card-body p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="avatar w-14 h-14 rounded-full bg-base-300">
                          <img src={request.sender.profilePic} alt={request.sender.fullName} />
                        </div>
                        <div>
                          <h3 className="font-semibold">{request.sender.fullName}</h3>
                          <div className="flex flex-wrap gap-1.5 mt-1">
                            <span className="badge badge-secondary badge-sm">
                              Native: {request.sender.nativeLanguage}
                            </span>
                            <span className="badge badge-outline badge-sm">
                              Learning: {request.sender.learningLanguage}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => acceptRequestMutation(request.id)}
                          disabled={isBusy}
                        >
                          Accept
                        </button>
                        <button
                          className="btn btn-ghost btn-sm"
                          onClick={() => rejectRequestMutation(request.id)}
                          disabled={isBusy}
                        >
                          <XIcon className="size-4" />
                          Reject
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        ) : (
          <NoNotificationsFound />
        )}
      </div>
    </div>
  );
};
export default NotificationsPage;
