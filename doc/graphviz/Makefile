DIAGS=$(wildcard *.dot)
TARGETS=$(DIAGS:.dot=.pdf)

all: $(TARGETS)

%.pdf: %.dot
	dot $< -Tpdf > $@

.PHONY clean:
	rm -f $(TARGETS)
	