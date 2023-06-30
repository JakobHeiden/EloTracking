package com.elorankingbot.patreon.model;

public class PatreonDataGsonModel {

    private Included[] included;

    public int getPledgedCents() {
        if (included == null) return 0;
        return included[0].attributes.currently_entitled_amount_cents;
    }

    private class Included {

        private Attributes attributes;
    }

    private class Attributes {

        private int currently_entitled_amount_cents;
    }
}
