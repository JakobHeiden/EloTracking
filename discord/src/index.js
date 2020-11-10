const Discord = require('discord.js');
const auth = require('../auth.json');
const client = new Discord.Client();

console.log('Attempting Discord login...');
client.login(auth.token);
client.on('ready', () => {
    console.log('Logged in as ' + client.user.tag + '!');
});

client.on('message', msg => {
    if (msg.content === '!ping') msg.channel.send('pong');
});