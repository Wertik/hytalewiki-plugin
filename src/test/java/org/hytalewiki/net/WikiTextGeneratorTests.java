package org.hytalewiki.net;

import com.lucaskjaerozhang.wikitext_parser.WikiTextParser;
import com.lucaskjaerozhang.wikitext_parser.ast.base.WikiTextNode;
import org.hytalewiki.WikiTextGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WikiTextGeneratorTests {

    // note: there's no non-breakable space in here and other special chars
    private final static String COPPER_WIKITEXT = """
            '''Copper''' is an early game [[mineral]]. It is worse than [[Iron]]. It can be found as [[copper ore]] while mining underground in any [[zone]] and smelted into [[iron ingot]]s at the [[furnace]].
            
            Copper is used to craft [[Copper Armor|armor]] and [[Copper Weapon|weapons]].
            
            == Armor ==
            {{DynamicItemList|category=*Armor&Copper}}
            
            == Weapons ==
            {{DynamicItemList|category=*Weapons&Copper}}
            
            == Usage ==
            
            === Crafting ingredient ===
            
            === [[Copper Ore]] ===
            {{Crafting usage|Copper Ore}}
            
            === [[Copper Ingot]] ===
            {{Crafting usage|Copper Ingot}}
            
            == Gallery ==
            <gallery>
            CopperArmor.jpg|A player wearing a copper armor.
            CopperSword.jpg|A player wielding a copper sword.
            CopperConcept.jpg|Concept art for some copper daggers.
            </gallery>
            
            == Navigation ==
            {{Navbox ore}}
            
            [[Category:Minerals]]
            [[Category:Mineable]]
            [[Category:Materials]]
            [[Category:Copper]]
            """;

    @Test
    public void sanitizesWikitext() {
        WikiTextGenerator wikiTextGenerator = new WikiTextGenerator();

        String result = wikiTextGenerator.sanitize(COPPER_WIKITEXT);

        assertEquals("""
                '''Copper''' is an early game [[mineral]]. It is worse than [[Iron]]. It can be found as [[copper ore]] while mining underground in any [[zone]] and smelted into [[iron ingot]]s at the [[furnace]].
                
                Copper is used to craft [[Copper Armor|armor]] and [[Copper Weapon|weapons]].
                
                == Armor ==
                
                == Weapons ==
                
                == Usage ==
                
                === Crafting ingredient ===
                
                === Copper Ore ===
                
                === Copper Ingot ===
                
                == Gallery ==
                <gallery>
                CopperArmor.jpgA player wearing a copper armor.
                CopperSword.jpgA player wielding a copper sword.
                CopperConcept.jpgConcept art for some copper daggers.
                </gallery>
                
                == Navigation ==
                
                """, result);
    }

    @Test
    public void parsesAST() {
        WikiTextGenerator wikiTextGenerator = new WikiTextGenerator();

        String cleaned = wikiTextGenerator.sanitize(COPPER_WIKITEXT);

        WikiTextNode root = assertDoesNotThrow(() -> (WikiTextNode) WikiTextParser.parse(cleaned));

        assertNotNull(root);
    }

    @Test
    // todo: requires the hytale logger to be set up
    @Disabled
    public void parsesBasicWikitext() {
        // todo: don't really have a nice way to put expectations over the AST or UI output
        // take inspiration from wikitext-parser and how it tests AST output

        WikiTextGenerator wikiTextGenerator = new WikiTextGenerator();

        assertNotNull(wikiTextGenerator.generate(COPPER_WIKITEXT));
    }
}
