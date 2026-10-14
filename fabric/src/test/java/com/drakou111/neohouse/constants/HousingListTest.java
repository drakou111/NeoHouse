package com.drakou111.neohouse.constants;

import com.drakou111.neohouse.actions.actiontypes.DeleteListEntriesAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HousingListTest {

    @Test
    void namesRoundTripThroughTheirListItems() {
        for (HousingList list : HousingList.values())
            assertThat(list.nameOf(list.displayName("shop"))).isEqualTo("shop");
    }

    @Test
    void commandsAreListedWithASlash() {
        assertThat(HousingList.COMMANDS.displayName("spawn")).isEqualTo("/spawn");
        assertThat(HousingList.COMMANDS.nameOf("/spawn")).isEqualTo("spawn");
        assertThat(HousingList.FUNCTIONS.nameOf("/odd")).isEqualTo("/odd");
    }

    @Test
    void onlyFunctionsCommandsAndMenusCanBeCleared() {
        assertThatThrownBy(() -> new DeleteListEntriesAction(HousingList.TEAMS)).isInstanceOf(IllegalArgumentException.class);
    }
}
