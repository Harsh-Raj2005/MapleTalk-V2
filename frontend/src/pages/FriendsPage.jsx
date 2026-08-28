import FriendsList from "../components/FriendsList";

const FriendsPage = () => {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="container mx-auto space-y-6">
        <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">Your Friends</h2>
        <FriendsList />
      </div>
    </div>
  );
};

export default FriendsPage;
