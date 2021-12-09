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
            <header>
                <H1Styled>Leaderboard: {gameData.name}</H1Styled>
            </header>
            <TableStyled>
                <tbody>
                <TopRowStyled>
                    <th>Rank</th>
                    <th>Name</th>
                    <th>Rating</th>
                </TopRowStyled>
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

const H1Styled = styled.h1 `
font-family: "Roboto Light",sans-serif;
background-color: lavender;
text-align: center;
margin: 0 2px;
height: 200%;
`

const TopRowStyled = styled.tr `
font-family: "Roboto Light",sans-serif;
`
