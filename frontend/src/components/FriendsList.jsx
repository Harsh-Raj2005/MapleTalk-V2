import { useQuery } from "@tanstack/react-query";
import { getUserFriends } from "../lib/api";
import FriendCard from "./FriendCard";
import NoFriendsFound from "./NoFriendsFound";

const FriendsList = () => {
  const { data: friends = [], isLoading } = useQuery({
    queryKey: ["friends"],
    queryFn: getUserFriends,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <span className="loading loading-spinner loading-lg" />
      </div>
    );
  }

  if (friends.length === 0) {
    return <NoFriendsFound />;
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      {friends.map((friend) => (
        <FriendCard key={friend.id} friend={friend} />
      ))}
    </div>
  );
};

export default FriendsList;
