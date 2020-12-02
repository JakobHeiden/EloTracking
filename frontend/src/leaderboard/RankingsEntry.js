import React from 'react';
import styled from 'styled-components/macro';

export default function RankingsEntry({player}) {
    return (
        <RowStyled>
            <td></td>
            <td>{player.name}</td>
            <td>{Number.parseFloat(player.rating).toFixed(0)}</td>
        </RowStyled>
    )
}

const RowStyled = styled.tr `
td {text-align: center};
`