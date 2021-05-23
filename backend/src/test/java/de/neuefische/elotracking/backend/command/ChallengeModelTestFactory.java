package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.ChallengeModel;

import java.util.ArrayList;
import java.util.List;

import static de.neuefische.elotracking.backend.command.SnowflakeTestFactory.*;

class ChallengeModelTestFactory {

    public static ChallengeModel create() {
        return new ChallengeModel(CHANNELID, CHALLENGERID, RECIPIENTID);
    }

    public static List<ChallengeModel> createList() {
        return new ArrayList<ChallengeModel>();
    }

    public static List<ChallengeModel> createList(ChallengeModel challenge) {
        List list = createList();
        list.add(challenge);
        return list;
    }
}
