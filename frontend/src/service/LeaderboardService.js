import axios from 'axios';

export const getRankings = (channelId) =>
    axios.get('/api/rankings/' + channelId)
        .then(response => response.data);

export const getGameData = (channelId) =>
    axios.get('api/gamedata/' + channelId)
        .then(response => response.data);