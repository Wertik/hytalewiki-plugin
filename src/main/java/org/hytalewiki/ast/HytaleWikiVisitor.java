package org.hytalewiki.ast;

import au.ellie.hyui.builders.GroupBuilder;
import au.ellie.hyui.builders.HyUIStyle;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.builders.UIElementBuilder;
import au.ellie.hyui.elements.LayoutModeSupported;
import com.lucaskjaerozhang.wikitext_parser.ast.base.WikiTextNode;
import com.lucaskjaerozhang.wikitext_parser.ast.format.Bold;
import com.lucaskjaerozhang.wikitext_parser.ast.layout.LineBreak;
import com.lucaskjaerozhang.wikitext_parser.ast.layout.XMLContainerElement;
import com.lucaskjaerozhang.wikitext_parser.ast.layout.XMLStandaloneElement;
import com.lucaskjaerozhang.wikitext_parser.ast.link.CategoryLink;
import com.lucaskjaerozhang.wikitext_parser.ast.link.WikiLink;
import com.lucaskjaerozhang.wikitext_parser.ast.list.WikiTextList;
import com.lucaskjaerozhang.wikitext_parser.ast.sections.Section;
import com.lucaskjaerozhang.wikitext_parser.ast.sections.Text;
import com.lucaskjaerozhang.wikitext_parser.visitor.WikiTextBaseASTVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HytaleWikiVisitor extends WikiTextBaseASTVisitor<UIElementBuilder<?>> {

    private AtomicInteger id = new AtomicInteger();

    private int nextId() {
        return this.id.getAndIncrement();
    }

    public void reset() {
        this.id.set(0);
    }

    private HyUIStyle labelStyle() {
        return new HyUIStyle().set("Wrap", true);
    }

    private LabelBuilder buildLabel(String content) {
        LabelBuilder label = LabelBuilder.label()
                // trim extra spaces
                .withText(content.replaceAll(" +", " "))
                .withStyle(this.labelStyle());

        label.withId("text-" + this.nextId());
        return label;
    }

    @Override
    public Optional<UIElementBuilder<?>> visitXMLContainerElement(XMLContainerElement element) {
        return Optional.empty();
    }

    @Override
    public Optional<UIElementBuilder<?>> visitXMLStandaloneElement(XMLStandaloneElement element) {
        // note: could break things like <nowiki>
        return Optional.empty();
    }

    @Override
    public Optional<UIElementBuilder<?>> visitText(Text text) {
        String content = text.getContent();
        if (content.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildLabel(content));
    }

    @Override
    public Optional<UIElementBuilder<?>> visitBold(Bold bold) {
        String content = bold.getChildren().stream()
                .filter(child -> child instanceof Text)
                .map(child -> ((Text) child).getContent())
                .collect(Collectors.joining(""));

        if (content.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildLabel(content));

//        seperating text cannot be inlined like html spans.
//
//        String content = bold.getChildren().stream()
//                .filter(child -> child instanceof Text)
//                .map(child -> ((Text) child).getContent())
//                .collect(Collectors.joining(""));
//
//        if (content.trim().isEmpty()) {
//            return Optional.empty();
//        }
//
//        LabelBuilder label = LabelBuilder.label()
//                .withStyle(this.labelStyle().setRenderBold(true));
//
//        // should only contain text
//        label.withText(content);
//
//        label.withId("bold-" + this.nextId());
//        return Optional.of(label);
    }

    @Override
    public Optional<UIElementBuilder<?>> visitLineBreak(LineBreak lineBreak) {
        return Optional.of(LabelBuilder.label().withId("line-break-" + this.nextId()).withText("\n"));
    }

    @Override
    public Optional<UIElementBuilder<?>> visitSection(Section section) {
        GroupBuilder group = GroupBuilder.group()
                .withLayoutMode(LayoutModeSupported.LayoutMode.TopScrolling);

        section.getAttributes().stream().filter(n -> n.getKey().equals("title")).findAny()
                .ifPresent(n -> {
                    String title = n.getValue();
                    group.addChild(buildLabel(title)
                            .withStyle(this.labelStyle().setRenderBold(true))
                            .withId("title-" + this.nextId()));
                });

        group.withId("section-" + this.nextId());
        return composeChildren(group, section.getChildren());
    }

    @Override
    public Optional<UIElementBuilder<?>> visitCategoryLink(CategoryLink categoryLink) {
        // todo: compose into a list and later add as buttons on the bottom of the page
        return super.visitCategoryLink(categoryLink);
    }

    @Override
    public Optional<UIElementBuilder<?>> visitWikiLink(WikiLink wikiLink) {
        return super.visitWikiLink(wikiLink);
    }

    @Override
    public Optional<UIElementBuilder<?>> visitWikiTextList(WikiTextList wikiTextList) {
        return super.visitWikiTextList(wikiTextList);
    }

    @Override
    protected Optional<UIElementBuilder<?>> visitChildren(List<WikiTextNode> children) {
        GroupBuilder group = GroupBuilder.group()
                .withLayoutMode(LayoutModeSupported.LayoutMode.TopScrolling);
        group.withId("parent-" + this.nextId());
        return composeChildren(group, children);
    }

    private Optional<UIElementBuilder<?>> composeChildren(GroupBuilder group, List<WikiTextNode> children) {
        if (children.size() == 1) {
            return children.getFirst().accept(this);
        }

        boolean anyNonEmptyText = false;

        List<LabelBuilder> neighboringText = new ArrayList<>();

        for (WikiTextNode child : children) {
            Optional<UIElementBuilder<?>> accept = child.accept(this);

            if (accept.isPresent()) {
                UIElementBuilder<?> element = accept.get();

                if (element instanceof LabelBuilder labelBuilder) {
                    // line breaks don't count as non-empty text
                    if (!labelBuilder.getId().startsWith("line-break")) {
                        anyNonEmptyText = true;
                    }
                    neighboringText.add(labelBuilder);
                } else {
                    neighboringText.stream()
                            .map(LabelBuilder::getText)
                            .reduce((a, b) -> a + b)
                            .ifPresent(t -> group.addChild(buildLabel(t)));
                    neighboringText.clear();

                    group.addChild(accept.get());
                    anyNonEmptyText = true;
                }
            }
        }

        if (!neighboringText.isEmpty()) {
            neighboringText.stream()
                    .map(LabelBuilder::getText)
                    .reduce((a, b) -> a + b)
                    .ifPresent(t -> group.addChild(buildLabel(t)));
        }

        if (!anyNonEmptyText) {
            return Optional.empty();
        }

        return Optional.of(group);
    }
}
