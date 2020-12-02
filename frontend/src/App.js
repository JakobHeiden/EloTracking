import React from 'react';
import {Route, Switch, useParams} from 'react-router-dom';
import Leaderboard from "./leaderboard/Leaderboard";

export default function App() {
  return (
     <Switch>
         <Route path="/:channelid" component={Leaderboard} />
     </Switch>
  );
};

