package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private final ItemRepository itemRepository;
    private final ItemValidator itemValidator;

    // WebDataBinder : 스프링의 파라미터 바인딩의 역할을 해주고 검증 기능도 내부에 포함한다.
    @InitBinder
    public void init(WebDataBinder dataBinder) {
        dataBinder.addValidators(itemValidator);
    }

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
//    @PostMapping("/add")
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


    /**
     * V2에서 오류 코드에 따른 메시지 처리 활용
     * FieldError에서 code와 argument 활용
     *
     * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
//    @PostMapping("/add")
    public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());


        // 검증 로직
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false,new String[]{"required.item.itemName"},null,null));
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.addError(new FieldError("item", "price",item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000, 1000000}, null));
        }

        if (item.getQuantity() == null || item.getQuantity() >= 9000) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999}, null));
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                // ObjectError : 특정 필드를 넘어서는 오류가 있는 경우에는 ObjectError를 생성해서 bindingResult에 담아두면 된다.
                bindingResult.addError(new ObjectError("item",new String[] {"totalPriceMin"},new Object[]{10000, resultPrice},null));
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
     * V2 오류 코드 와 메시지처리 #2
     * bindingResult의 RejectValue와 Reject
     * BindingReuslt는 검증해야할 객체인 target을 알고 있다(log 참고) 그러므로 target의 대한 정보는 굳이 작성해주지 않아도 된다.
     * 축약된 오류 코드 range.item.price -> range 로 간단하게 입력가능
     * -> MessageCodeResolver
     * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
//    @PostMapping("/add")
    public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 가격에 숫자가 아닌 값을 넣었을 때 '숫자를 입력하세요' 메시지만 보일 수 있도록 설정
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }


        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());


        // 검증 로직
//        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required"); 한줄로도 가능! (Empty와 공백같이 단순한 기능을 제공한다.)
        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.rejectValue("itemName","required");
        }

        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
        }

        if (item.getQuantity() == null || item.getQuantity() >= 9000) {
            bindingResult.rejectValue("quantity", "max", new Object[]{9999}, null);
        }

        // 특정 필드가 아닌 복합 룰 검증
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice},null);
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
     * Validator 분리 #1
     * itemValidator
     * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
//    @PostMapping("/add")
    public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        itemValidator.validate(item, bindingResult);

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
     * Validator #2
     * @Validated 어노테이션을 추가해주면 addItem5 와 똑같이 동작한다.
     *  : 검증기를 실행하라~ 라는 어노테이션이다.
     * @param item
     * @param bindingResult
     * @param redirectAttributes
     * @param model
     * @return
     */
    @PostMapping("/add")
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
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

