import React, {useState, useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {getRankings, getGameData} from '../service/LeaderboardService.js';
import RankingsEntry from "./RankingsEntry";
import styled from 'styled-components/macro';

export default function Leaderboard() {
    const {channelid: channelId} = useParams();
    const [rankings, setRankings] = useState([]);
    const [gameData, setGameData] = useState({name:''});

    useEffect(() => {
        getGameData(channelId).then(setGameData);
        getRankings(channelId).then(setRankings);
    }, [channelId]);

    return (
        <>
            <HeaderStyled>
                <h1>Leaderboard: {gameData.name}</h1>
            </HeaderStyled>
            <TableStyled>
                <tbody>
                <tr>
                    <th>Rank</th>
                    <th>Name</th>
                    <th>Rating</th>
                </tr>
                {rankings.map(player => (
                    <RankingsEntry player={player}/>))}
                </tbody>
            </TableStyled>
        </>
    );
}

const TableStyled = styled.table `
counter-reset: rowNumber;
tr:not(:first-child){counter-increment: rowNumber};
tr td:first-child::before {content: counter(rowNumber)}

tr:nth-child(even){background-color: lavender};
tr:nth-child(odd){background-color: cornflowerblue};

width: 100%;
font-size: 200%;
`

const HeaderStyled = styled.header `
p:{background-color: lavender};
`
