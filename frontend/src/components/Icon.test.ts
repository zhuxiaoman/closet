import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Icon from "./Icon.vue";

describe("Icon", () => {
  it("renders lucide icon by kebab-case name", () => {
    const wrapper = mount(Icon, { props: { name: "home" } });
    expect(wrapper.html()).toContain("svg");
  });

  it("falls back to HelpCircle for unknown names", () => {
    const wrapper = mount(Icon, { props: { name: "unknown-icon" } });
    expect(wrapper.html()).toContain("svg");
  });

  it("passes size to svg", () => {
    const wrapper = mount(Icon, { props: { name: "home", size: 32 } });
    const svg = wrapper.find("svg");
    expect(svg.attributes("width")).toBe("32");
  });

  it("renders sparkles icon (sanity for plan mention)", () => {
    const wrapper = mount(Icon, { props: { name: "sparkles" } });
    expect(wrapper.html()).toContain("svg");
  });
});