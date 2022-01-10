package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private final ItemRepository itemRepository;

    @GetMapping
    public String items(Model model) {
        List<Item> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "validation/v2/items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/item";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Item());
        return "validation/v2/addForm";
    }


    /**
     * V2
     * BindingResult Item에 바인딩된 결과가 BindingResult에 담긴다!
     * BindingReulst 파라미터 위치는 ModelAttribute 객체 바로 뒤에 와야 한다!!!! (순서 중요)
     * BindingResult는 스프링이 제공하는 검증 오류를 보관하는 객체이다. 그래서 MddelAttribute에 데이터 바인딩 시 오류 발생해도 컨트롤러가 호출된다~~
     * BindingResult, FieldError, ObjectError를 통해서 오류 메시지를 처리하였는데 여기서 다음 문제는
     * 이제 오류가 발생하면 사용자가 입력했던 내용이 모두 사라져서 사용자가 어떤 값을 입력해서 저장이 안됐는지 확인할 수 가 없다..!
     * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
//    @PostMapping("/add")
    public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수 입니다."));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }

        if (item.getQuantity() == null || item.getQuantity() >= 9000) {
            bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // ObjectError : 특정 필드를 넘어서는 오류가 있는 경우에는 ObjectError를 생성해서 bindingResult에 담아두면 된다.
                bindingResult.addError(new ObjectError("item","가격 * 수량의 합은 1,0000원 이상이어야 합니다. 현재 값 = " + resultPrice ));
           }
        }

        // 검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    /**
     * V2에서 FieldError, ObjectError 활용버전
     * V2 버전에서 사용자가 입력했다가 오류 발생하면 오류 메시지 보여준 다음에 값이 사라져서 사용자가 어떤 값을 입력했는지 확인할 수 없었는데
     * BindingReuslt에 파라미터 중 rejectedValue에 사용자가 입력한 값을 담아주면
     * 오류 발생한 후에 값이 사라지는것이 아니라 사용자가 입력된 값이 남아 확인 가능하다.
     * bindingFailure 를 false로 설정한 이유는 데이터 자체는 정상적으로 들어오고 있기 때문에 바인딩 실패인지, 검증실패인지 구분하기 위해 설정하는 값이다.
     * 지금은 바인딩 실패한 것은 아니었기 때문에 false로 들어가면 된다.
     * 만약 바인딩이 숫자가 와야하는데 문자가 오면 바인딩 실패이므로 true로 들어간다.
    * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
    @PostMapping("/add")
    public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false,null,null,"상품 이름은 필수 입니다."));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price",item.getPrice(), false, null, null, "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }

        if (item.getQuantity() == null || item.getQuantity() >= 9000) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null, "수량은 최대 9,999 까지 허용합니다."));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // ObjectError : 특정 필드를 넘어서는 오류가 있는 경우에는 ObjectError를 생성해서 bindingResult에 담아두면 된다.
                bindingResult.addError(new ObjectError("item",null,null,"가격 * 수량의 합은 1,0000원 이상이어야 합니다. 현재 값 = " + resultPrice ));
            }
        }

        // 검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        // 성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }



    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute Item item) {
        itemRepository.update(itemId, item);
        return "redirect:/validation/v2/items/{itemId}";
    }

}

